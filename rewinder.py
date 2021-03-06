import util
import git

'''
    GitRewinder expects the path of the project in which the sha hashes can be used by git to rewind a project, what type the rewinder is for logging (Version, MasterOnly, InitialOnly, 2Week, Monthly, etc.), the uniqueSourceID, and a list of (commitTime,sha,sourceJSON) tuples with most recent on top.
'''


# notice in this class there is no database access or logging done directly, only enough code to accomplish the effect of rewinding the source, and providing unstructured data about what the source was
class GitRewinder:
    def __init__(self, path, rewinder_type, uniqueSourceID, stack):
        self.stack = stack
        self.gitRepo = git.Git(path)
        self.rewinder_type = rewinder_type
        self.sourceJson = "{initialized}"
        self.uniqueSourceID = uniqueSourceID
        self.sha = "initialized"
        self.cuteID = util.getCuteID(9)

    def rewind(self):
        if not self.stack:
            return False
        commitTuple = self.stack.pop()
        self.sha = commitTuple[1]
        self.sourceJson = commitTuple[2]

        # Note that --hard is needed to get rid of files from previous commits
        self.gitRepo.reset("--hard", self.sha)
        return True

    def getUniqueSourceID(self):
        return self.uniqueSourceID

    # after much trial and error making complex SQL tables, it seems like a little unstructured data might be the best way to characterize important state for various sources like Bitbucket, Sourceforge, etc.  Note that the rewinder itself never gets past the Tourist - it passes content to the scanner and steers the rewinding process with `rewind()'
    def getSourceJSON(self):
        return self.sourceJson

    def log(self):
        return "GitRe - log, type: " + self.rewinder_type + " ID: " + self.cuteID + " uniqueSourceID: " + str(self.getUniqueSourceID()) + " sha: " + str(self.sha)


# does not use sourceJSON
class GitRewinderBasic:
    def __init__(self, path, repoID, stack):
        self.stack = stack
        self.gitRepo = git.Git(path)
        self.repoID = repoID
        self.sha = "initialized"
        self.cuteID = util.getCuteID(9)

    def rewind(self):
        if not self.stack:
            return False
        commitTuple = self.stack.pop()
        self.sha = commitTuple[1]

        # Note that --hard is needed to get rid of files from previous commits
        self.gitRepo.reset("--hard", self.sha)
        return True

    def log(self):
        return "GitRe - log, ID: " + self.cuteID + " repoID: " + str(self.repoID) + " sha: " + str(self.sha)


# This is a mock, not a stub because its values depend on externalities
class MockRewinder:
    def __init__(self, repoID, sourceJSON):
        self.initial = True
        self.repoID = repoID
        self.sourceJSON = sourceJSON

    def rewind(self):
        if self.initial:
            self.initial = False
            return True
        else:
            return False

    def getUniqueSourceID(self):
        return -1

    # after much trial and error making complex SQL tables, it seems like a little unstructured data might be the best way to characterize important state for various sources like Bitbucket, Sourceforge, etc.  Note that the rewinder itself never gets past the Tourist - it passes content to the scanner and steers the rewinding process with `rewind()'
    def getSourceJSON(self):
        return self.sourceJSON

    def log(self):
        return "FakeRe - log, repoID: " + str(self.repoID)
